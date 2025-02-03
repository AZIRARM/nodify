export class StringUtils {
  public static isEmpty(text:string){
    if(text === null || text === undefined || text.length === 0){
      return true;
    }
    return false;
  }
  public static isNotEmpty(text:string){
    return !this.isEmpty(text);
  }
}
