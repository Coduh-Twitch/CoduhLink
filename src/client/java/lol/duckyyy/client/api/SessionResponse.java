package lol.duckyyy.client.api;


public class SessionResponse {
    public static class User {
        public String id;
        public String login;
        public String display_name;
        public String type;
        public String broadcaster_type;
        public String description;
        public String profile_image_url;
        public String offline_image_url;
        public int view_count;
        public String created_at;
    }
    public static class AsUser {
        public String access_token;
        public SessionResponse.User user;
    }
}
