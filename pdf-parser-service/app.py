import os
import sys

# ==============================================================================
# WINDOWS DLL RESOLUTION PATCH (Chống lỗi WinError 127 & Tự động sửa thiếu cuDNN)
# ==============================================================================
if sys.platform == 'win32':
    os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"
    
    # 1. Quét qua các thư mục site-packages hoạt động để nạp DLL của torch và nvidia-cudnn
    try:
        import site
        for site_dir in site.getsitepackages():
            # Nạp DLL của PyTorch (nếu có)
            torch_lib = os.path.join(site_dir, 'torch', 'lib')
            if os.path.exists(torch_lib):
                os.add_dll_directory(torch_lib)
                print(f"🔧 Windows DLL Patch: Đã nạp thành công DLL từ {torch_lib}")
                
            # TỰ ĐỘNG KHẮC PHỤC THIẾU CUDNN: Quét nạp DLL từ thư viện nvidia-cudnn-cu12 nếu được cài qua pip
            cudnn_bin = os.path.join(site_dir, 'nvidia', 'cudnn', 'bin')
            if os.path.exists(cudnn_bin):
                os.add_dll_directory(cudnn_bin)
                print(f"🚀 Windows DLL Patch (cuDNN): Đã liên kết thành công {cudnn_bin} vào hệ thống!")
    except Exception as dll_err:
        print(f"⚠️ Cảnh báo cấu hình DLL hệ thống: {dll_err}")

import re
import shutil
import tempfile
import numpy as np
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
import fitz  # PyMuPDF
from paddleocr import PaddleOCR

app = FastAPI(
    title="GPU-Accelerated AI PDF Parser Service",
    description="Ultra-lightweight FastAPI service with GPU-accelerated PaddleOCR and Page-based structural chunking.",
    version="2.1.0"
)

# Khởi tạo PaddleOCR chạy 100% trên GPU CUDA (Đã tối ưu bộ nhớ)
try:
    ocr_engine = PaddleOCR(use_angle_cls=True, lang="vi", use_gpu=True, gpu_mem=1000)
    print("🚀 PaddleOCR khởi tạo thành công chạy 100% trên GPU CUDA!")
except Exception as e:
    print(f"⚠️ Cảnh báo khởi tạo GPU: {e}. Chuyển sang CPU.")
    ocr_engine = PaddleOCR(use_angle_cls=True, lang="vi", use_gpu=False)

def extract_structured_page_content(page, page_num, temp_dir) -> tuple:
    """
    Trích xuất cấu trúc văn bản phân cấp (Hierarchical Structure) và Bảng biểu (Tables) chất lượng cao:
    1. Loại bỏ rác Header / Footer / Số trang tự động bằng tọa độ Y và bộ lọc Regex/Từ khóa.
    2. Nhận dạng Bảng biểu (Tables) bằng thuật toán hình học native của PyMuPDF và chuyển đổi sang Markdown Tables.
    3. Nhận dạng Tiêu đề lớn/nhỏ (Headings) dựa trên cỡ chữ (Font size) và Bold.
    4. Tránh trích xuất trùng lặp văn bản phẳng nằm bên trong Bảng biểu.
    """
    rect = page.rect
    width, height = rect.width, rect.height
    
    # 1. Tìm các bảng biểu trên trang bằng thuật toán hình học native của PyMuPDF
    tables = page.find_tables()
    table_bboxes = []
    markdown_tables = []
    
    for t in tables:
        table_bboxes.append(t.bbox)
        headers = t.header.names
        rows = t.extract()
        if not rows:
            continue
            
        md = []
        # Xây dựng dòng tiêu đề cột
        clean_headers = [str(h).strip() if h else "" for h in headers]
        # Nếu dòng tiêu đề trống hoàn toàn, tạo tiêu đề mặc định
        if not any(clean_headers):
            clean_headers = [f"Cột {i+1}" for i in range(len(clean_headers))]
        md.append("| " + " | ".join(clean_headers) + " |")
        md.append("| " + " | ".join(["---"] * len(clean_headers)) + " |")
        
        # Thêm các dòng dữ liệu (loại bỏ dòng header trùng lặp bên ngoài nếu có)
        start_idx = 1 if t.header.external else 0
        for r in rows[start_idx:]:
            if r is None:
                continue
            clean_row = [str(cell).strip().replace("\n", " ").replace("|", "\\|") if cell else "" for cell in r]
            md.append("| " + " | ".join(clean_row) + " |")
            
        markdown_tables.append({
            "bbox": t.bbox,
            "markdown": "\n".join(md)
        })

    # 2. Trích xuất text có phân cấp bằng cấu trúc "dict"
    text_dict = page.get_text("dict")
    blocks = text_dict.get("blocks", [])
    
    # Tính font size phổ biến nhất (body text size) để phát hiện Heading chuẩn xác
    sizes = []
    for b in blocks:
        for line in b.get("lines", []):
            for span in line.get("spans", []):
                text = span.get("text", "").strip()
                if len(text) > 3:
                    sizes.append(round(span.get("size", 12)))
                    
    body_size = 12
    if sizes:
        body_size = max(set(sizes), key=sizes.count)

    structured_elements = []
    active_heading = ""
    
    # Bộ lọc các từ khóa rác Header / Footer thường lặp lại
    garbage_keywords = [
        "hanoi university of science and technology",
        "school of information and communication technology",
        "soict", "bach khoa", "dai hoc bach khoa",
        "hust", "postgraduate", "undergraduate"
    ]
    
    for b in blocks:
        # Chỉ xử lý các block chứa text
        if "lines" not in b:
            continue
            
        bbox = b.get("bbox", (0, 0, 0, 0))
        x0, y0, x1, y1 = bbox
        
        # A. Lọc bỏ Header & Footer dựa trên tọa độ Y (top 8% và bottom 8% trang)
        is_header = y0 < 0.08 * height
        is_footer = y1 > 0.92 * height
        
        block_text_raw = ""
        for line in b.get("lines", []):
            for span in line.get("spans", []):
                block_text_raw += span.get("text", "")
                
        block_text_clean = block_text_raw.strip().lower()
        
        if is_header or is_footer:
            # Loại bỏ nếu chứa từ khóa rác hoặc chỉ chứa số trang
            if any(k in block_text_clean for k in garbage_keywords) or re.match(r'^\d+$', block_text_clean):
                continue
                
        # B. Kiểm tra xem block text này có bị trùng lặp nằm trong bảng biểu nào không
        is_inside_table = False
        for t_bbox in table_bboxes:
            # Kiểm tra nếu tọa độ block nằm hoàn toàn trong bbox của bảng
            if x0 >= t_bbox[0] - 3 and y0 >= t_bbox[1] - 3 and x1 <= t_bbox[2] + 3 and y1 <= t_bbox[3] + 3:
                is_inside_table = True
                break
        if is_inside_table:
            continue
            
        # C. Định dạng phân cấp Heading / Lists
        block_spans = []
        for line in b.get("lines", []):
            for span in line.get("spans", []):
                block_spans.append(span)
                
        if not block_spans:
            continue
            
        # Lấy font size và font name của block
        max_size = max(span.get("size", 12) for span in block_spans)
        max_font = block_spans[0].get("font", "")
        is_bold = "bold" in max_font.lower() or "heavy" in max_font.lower() or "medium" in max_font.lower()
        
        block_content = ""
        for line in b.get("lines", []):
            line_text = ""
            for span in line.get("spans", []):
                line_text += span.get("text", "")
            block_content += line_text + "\n"
        block_content = block_content.strip()
        
        if not block_content:
            continue

        # Định dạng Header Markdown
        if max_size > body_size + 2.5 and is_bold:
            if max_size > body_size + 6:
                block_content = f"# {block_content}"
            else:
                block_content = f"## {block_content}"
            active_heading = block_content.replace("#", "").strip()
        else:
            # Chuẩn hóa Bullet Points cho từng dòng trong block
            lines = block_content.split("\n")
            clean_lines = []
            for line in lines:
                line_strip = line.strip()
                # Phát hiện các dòng bắt đầu bằng ký tự bullet hình học chuẩn (tuyệt đối không bao gồm chữ o hoặc O)
                if re.match(r'^[•\-\*▪◦■——–]\s*', line_strip):
                    line_clean = re.sub(r'^[•\-\*▪◦■——–]\s*', '* ', line_strip)
                    clean_lines.append(line_clean)
                else:
                    clean_lines.append(line)
            block_content = "\n".join(clean_lines)
            
        structured_elements.append({
            "type": "text",
            "y": y0,
            "content": block_content
        })

    # D. Thêm các bảng biểu Markdown vào danh sách theo trục tọa độ Y của chúng
    for t in markdown_tables:
        structured_elements.append({
            "type": "table",
            "y": t["bbox"][1],
            "content": t["markdown"]
        })
        
    # Sắp xếp các phần tử từ trên xuống dưới theo thứ tự đọc thực tế trục Y
    structured_elements.sort(key=lambda x: x["y"])
    
    # Nối các phần tử thành Markdown hoàn chỉnh của trang
    final_page_markdown = ""
    for elem in structured_elements:
        final_page_markdown += elem["content"] + "\n\n"
        
    return final_page_markdown.strip(), active_heading

def chunk_page_text(text: str, page_num: int, active_heading: str = "", max_chars: int = 1000, overlap: int = 200) -> list:
    """
    Thuật toán cắt văn bản theo trang (Page-based Structural Chunking) kết hợp Contextual Prefixing:
    1. Giữ nguyên tính toàn vẹn của trang tài liệu để phục vụ Citation RAG chính xác.
    2. Nếu nội dung trang quá dài (vượt quá max_chars), tự động chia nhỏ bằng Sliding Window với độ đè dịch (overlap) để không mất ngữ cảnh giữa các đoạn.
    3. Thêm tiền tố Tiêu đề hoạt động (Active Heading) vào từng chunk để bảo toàn cấu trúc phân cấp cho RAG.
    """
    text = text.strip()
    if not text:
        return []
        
    prefix = ""
    if active_heading:
        prefix = f"### [Chủ đề: {active_heading}] (Trang {page_num})\n"
    else:
        prefix = f"### (Trang {page_num})\n"
        
    # Tính độ dài tối đa thực tế của phần nội dung sau khi trừ đi tiền tố
    effective_max_chars = max(300, max_chars - len(prefix))
    
    if len(text) <= effective_max_chars:
        return [{
            "content": prefix + text,
            "sourceCitation": f"Trang {page_num}"
        }]
        
    # Áp dụng Sliding Window
    chunks = []
    start = 0
    while start < len(text):
        end = start + effective_max_chars
        chunk_content = text[start:end].strip()
        chunks.append({
            "content": prefix + chunk_content,
            "sourceCitation": f"Trang {page_num}"
        })
        start += (effective_max_chars - overlap)
        
    return chunks

@app.post("/api/v1/parser/extract")
async def extract_pdf_content(file: UploadFile = File(...)):
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(status_code=400, detail="Chỉ chấp nhận định dạng PDF.")

    temp_dir = tempfile.mkdtemp()
    temp_pdf_path = os.path.join(temp_dir, file.filename)
    
    try:
        # 1. Lưu file tạm
        with open(temp_pdf_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

        # 2. Mở tài liệu PDF
        doc = fitz.open(temp_pdf_path)
        pages_result = []

        try:
            for page_idx, page in enumerate(doc):
                page_num = page_idx + 1
                
                # 1. Trích xuất cấu trúc văn bản phân cấp (Hierarchical Structure) và Bảng biểu (Tables) chất lượng cao dạng Markdown
                # Đồng thời loại bỏ rác Header / Footer / Số trang tự động bằng bộ lọc tọa độ Y
                page_text, active_heading = extract_structured_page_content(page, page_num, temp_dir)

                # Tự động phát hiện trang quét (Scanned Page / Image Page) nếu số lượng ký tự văn bản quá ít
                raw_chars = len(page_text.strip())
                if raw_chars < 50:
                    print(f"📷 Phát hiện trang {page_num} là ảnh quét (chỉ có {raw_chars} ký tự). Chạy PaddleOCR trên GPU CUDA...")
                    
                    pix = page.get_pixmap(dpi=150)
                    temp_img_path = os.path.join(temp_dir, f"page_{page_num}.png")
                    pix.save(temp_img_path)
                    
                    # Thực hiện OCR tăng tốc bằng GPU (Có cơ chế dự phòng tự động chuyển sang CPU nếu thiếu thư viện cuDNN)
                    try:
                        ocr_result = ocr_engine.ocr(temp_img_path, cls=True)
                    except Exception as gpu_err:
                        print(f"⚠️ Lỗi chạy GPU OCR (Có thể thiếu cuDNN hoặc cấu hình CUDA sai lệch): {gpu_err}")
                        print("🔄 Tự động chuyển sang chạy dự phòng bằng CPU OCR để tránh lỗi HTTP 500...")
                        try:
                            # Khởi tạo nhanh bộ xử lý OCR dự phòng bằng CPU
                            cpu_ocr_engine = PaddleOCR(use_angle_cls=True, lang="vi", use_gpu=False)
                            ocr_result = cpu_ocr_engine.ocr(temp_img_path, cls=True)
                        except Exception as cpu_err:
                            print(f"❌ Lỗi nghiêm trọng khi chạy OCR bằng CPU: {cpu_err}")
                            ocr_result = None

                    ocr_texts = []
                    if ocr_result and ocr_result[0]:
                        for line in ocr_result[0]:
                            ocr_texts.append(line[1][0]) # Lấy nội dung text nhận dạng được
                    
                    page_text = "\n".join(ocr_texts).strip()
                    active_heading = ""  # Với trang quét ảnh thì tạm thời không có heading động

                # 3. Phân mảnh văn bản theo Trang (Page-based) kết hợp Contextual Prefixing
                page_chunks = chunk_page_text(page_text, page_num, active_heading)
                
                # Đóng gói dữ liệu kết quả của trang (Khớp chính xác với Java DTO)
                pages_result.append({
                    "page": page_num,
                    "rawContent": page_text,
                    "chunks": [c["content"] for c in page_chunks]
                })
        finally:
            # GIẢI PHÓNG FILE KHÓA TRÊN WINDOWS: Đóng tài liệu PDF ngay lập tức sau khi xử lý xong
            doc.close()

        return JSONResponse(status_code=200, content={
            "success": True,
            "totalPages": len(pages_result),
            "pages": pages_result
        })

    except Exception as e:
        print(f"❌ Lỗi xử lý PDF: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống khi bóc tách PDF: {str(e)}")
        
    finally:
        # Dọn dẹp tệp tạm
        if os.path.exists(temp_dir):
            try:
                shutil.rmtree(temp_dir)
            except Exception as clean_err:
                print(f"⚠️ Cảnh báo dọn dẹp thư mục tạm: {clean_err}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="127.0.0.1", port=8090, reload=True)
