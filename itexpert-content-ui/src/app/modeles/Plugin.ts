import {PluginFile} from "./PluginFile";

export class Plugin {
  id: string;
  enabled: boolean;
  editable: boolean;
  description: string;
  name: string;
  code: string;
  entrypoint: string;
  modificationDate: string;
  creationDate: string;
  modifiedBy: string;
  deleted: boolean;

  resources:PluginFile[];
}
