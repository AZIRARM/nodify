export class  Env  {
  public static EXPERT_CONTENT_CORE_URL:string = "./core";
  public static EXPERT_CONTENT_API_URL:string = "./api";
  public static EXPERT_CONTENT_AUTHENTICATION_URL:string = "./authentication";

  public static isProd() {
    return false;
  }
  public static isDev() {
    return true;
  }
}
