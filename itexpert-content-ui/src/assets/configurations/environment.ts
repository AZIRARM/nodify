export class Env {
  public static EXPERT_CONTENT_CORE_URL: string = "http://localhost:8080/v0";
  public static EXPERT_CONTENT_API_URL: string = "http://localhost:1080";
  public static EXPERT_CONTENT_CORE_WEBSOCKET: string = "ws://localhost:8080/ws";
  public static EXPERT_CONTENT_AUTHENTICATION_URL: string = "http://localhost:8080/authentication";
  public static SUBSCRIBE_ENABLED: string = "false";

  public static isProd() {
    return false;
  }
  public static isDev() {
    return true;
  }
}
