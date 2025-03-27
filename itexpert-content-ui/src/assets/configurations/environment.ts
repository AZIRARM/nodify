export class  Env  {
  public static EXPERT_CONTENT_CORE_URL:string = "http://localhost:8080/v0";
  public static EXPERT_CONTENT_API_URL:string = "http://localhost:9080/v0";
  public static EXPERT_CONTENT_AUTHENTICATION_URL:string = "http://localhost:8080/authentication";
  public static GEMINI_API_KEY:string="AIzaSyD64eox6ieKuauUScCUk-cRBRUjKC2kcjk";
  public static DEEPSEEK_API_KEY:string="sk-6a790dad55024ccbbd4c7e09b9546052";
  public static DEEPSEEK_API_URL:string="https://api.deepseek.com";

  public static isProd() {
    return false;
  }
  public static isDev() {
    return true;
  }
}
