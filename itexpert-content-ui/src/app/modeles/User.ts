export class User {
  public id: string;
  public name: string;
  public firstname: string;
  public lastname: string;
  public email: string;
  public password: string;
  public validated: boolean;
  public roles: string[];
  public projects: string[];
  public params: UserParams;
}


export class UserParams {
  public chatBotUrl: string;
  public chatBotApiKey: string;
  public chatBotModel: string;
  public chatbotEnabled: boolean;
}