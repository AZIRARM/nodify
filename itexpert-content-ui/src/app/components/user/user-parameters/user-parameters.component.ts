import {Component, OnInit} from '@angular/core';
import {Parameters} from "../../../modeles/Parameters";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {ParametersService} from "../../../services/ParametersService";
import {User} from "../../../modeles/User";

@Component({
  selector: 'app-user-parameters',
  templateUrl: './user-parameters.component.html',
  styleUrls: ['./user-parameters.component.css']
})
export class UserParametersComponent implements OnInit {
  parameters: Parameters;
  user: User;

  constructor(
    private translate: TranslateService,
    private parametersService: ParametersService,
    private loggerService: LoggerService
  ) {
  }

  ngOnInit(): void {
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userInfo')))
      )
    );

    this.parametersService.getByUserId(this.user.id).subscribe(
      (data: any) => {
        if (data) {
          this.parameters = data;
        }
      },
      error => {
        console.error(error);
      }
    );
  }

  save() {
    this.parametersService.save(this.parameters).subscribe(
      (data: any) => {
        if (data) {
          this.translate.get("SAVE_SUCCESS").subscribe(trad => {
            this.loggerService.success(trad);
          })
        }
      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(trad => {
          this.loggerService.success(trad);
        })
      }
    );
  }
}
