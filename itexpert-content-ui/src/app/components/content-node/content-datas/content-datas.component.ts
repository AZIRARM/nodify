import {Component, Input} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {ContentNode} from "../../../modeles/ContentNode";

@Component({
  selector: 'app-content-datas',
  templateUrl: './content-datas.component.html',
  styleUrl: './content-datas.component.css'
})
export class ContentDatasComponent {

  @Input() contentNode: ContentNode;

  constructor(private translate: TranslateService,
              private loggerService: LoggerService,
              private contentNodeService: ContentNodeService
  ) {
  }

  deleteData(code: string, key: string) {
    this.contentNodeService.deleteData(code, key).subscribe(
      (response: any) => {

        this.contentNode = response;
        this.translate.get("DELETE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
        })

      },
      error => {
        this.translate.get("DELETE_ERROR").subscribe(trad => {
          this.loggerService.error(trad);
        })
      });
  }

}
