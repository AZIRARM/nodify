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
  public static generateCustomRandomString(length:number) {
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
      result += characters.charAt(Math.floor(Math.random() * characters.length));
    }
    return result;
  }
}
