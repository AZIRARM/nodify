import {ContentUrl} from "./ContentUrl";
import {Value} from "./Value";
import {Rule} from "./Rule";
import {ContentFile} from "./ContentFile";
import {Translation} from "./Translation";

export class ContentNode {

  public id: string;
  public parentCode: string;
  public parentCodeOrigin: string;

  public code: string;

  public environmentCode: string;

  public type: string;

  public language: string;
  public title: string;
  public description: string;
  public iconUrl: string;
  public redirectUrl: string;
  public pictureUrl: string;
  public content: string;
  public creationDate: number;
  public modificationDate: number;
  public modifiedBy: string;
  public urls: ContentUrl[];

  public snapshot: boolean;


  public file: ContentFile;

  public tags: string[];
  public values: Value[];
  public roles: string[];
  public rules: Rule[];

  public datas: Value[];

  public published: ContentNode;
  public publicationDate: number;
  public version: string;
  public status: string;

  public translations: Translation[];

  public userName: string;
}
