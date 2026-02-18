export class  Env  {
  public static EXPERT_CONTENT_CORE_URL:string = "http://localhost:8880/v0";
  public static EXPERT_CONTENT_API_URL:string = "http://localhost:8890";
  public static EXPERT_CONTENT_CORE_WEBSOCKET:string = "ws://localhost:8880/ws";
  public static EXPERT_CONTENT_AUTHENTICATION_URL:string = "http://localhost:8880/authentication";

  public static isProd() {
    return false;
  }
  public static isDev() {
    return true;
  }
}
