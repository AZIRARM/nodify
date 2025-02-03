export class  Env  {
  public static EXPERT_CONTENT_CORE_URL:string = "http://localhost:8080/v0";
  public static EXPERT_CONTENT_API_URL:string = "http://localhost:9080/v0";
  public static EXPERT_CONTENT_AUTHENTICATION_URL:string = "http://localhost:8080/authentication";

  public static isProd() {
    return false;
  }
  public static isDev() {
    return true;
  }
}
