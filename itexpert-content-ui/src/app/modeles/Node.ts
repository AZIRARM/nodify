import {Value} from "./Value";
import {Rule} from "./Rule";
import {ContentNode} from "./ContentNode";
import {Translation} from "./Translation";

export class Node {
  public id: string;
  public parentCode: string;
  public parentCodeOrigin: string;
  public name: string;
  public code: string;
  public environmentCode: string;
  public description: string;
  public defaultLanguage: string;
  public type: string;
  public subNodes: string[];
  public creationDate: number;
  public modificationDate: number;
  public modifiedBy: string;
  public snapshot: boolean;

  public tags: string[];
  public values: Value[];
  public roles: string[];
  public rules: Rule[];
  public contents: ContentNode[];
  public languages: string[];

  public publicationDate: number;
  public version: string;
  public status: string;
  public publicationStatus: string;

  public translations: Translation[];

  public userName: string;
}

