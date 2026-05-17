package com.hust.commonlibrary.constant;

public interface AppConstants {
    String DEFAULT_LANGUAGE = "vi";
    String SYSTEM = "system";

    String BACKEND_PREFIX = "/api/v1";
    String PHONE_NUMBER = "^\\+?[0-9]{10,15}$";

    String FRONTEND_HOST = "http://localhost:3000";
    String BACKEND_HOST = "http://localhost:8088";

    String DEFAULT_PAGE_NUMBER = "1";
    String DEFAULT_PAGE_SIZE = "15";
    String DEFAULT_SORT = "id,desc";

    String DEFAULT_AVATAR = "https://th.bing.com/th/id/OIP.dDKYQqVBsG1tIt2uJzEJHwHaHa?w=182&h=182&c=7&r=0&o=7&pid=1.7&rm=3";
    int DEFAULT_MAX_TOKEN_NUM = 3;

    String[] PUBLIC_API_PATH = {
            "/auth/register",
            "/auth/logout",
            "/auth/refresh-token"
    };

    interface Token_Constants {
        String ACCESS_TOKEN = "access_token";
        String REFRESH_TOKEN = "refresh_token";
    }

    interface Redis_Constants {
        String APP_PREFIX = "elearning:";
    }

    interface Role_Constants {
        String ROLE_STUDENT = "STUDENT";
        String ROLE_INSTRUCTOR = "INSTRUCTOR";
        String ROLE_ADMIN = "ADMIN";
    }

    interface Field_Constants {
        String ID = "id";
        String SLUG = "slug";
        String NAME = "name";
        String EMAIL = "email";
        String CODE = "code";
    }

    interface Resource_Constants {
        String COURSE = "Course";
        String USER = "User";
        String CATEGORY = "Category";
        String LESSON = "Lesson";
        String ROLE = "Role";
        String SESSION = "Session";
    }

    interface Upload_Strategies {

    }
}
