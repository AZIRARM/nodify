import {Component, OnInit} from '@angular/core';
import {Parameters} from "../../../modeles/Parameters";
import {TranslateService} from "@ngx-translate/core";
import {LoggerService} from "../../../services/LoggerService";
import {ParametersService} from "../../../services/ParametersService";
import {User} from "../../../modeles/User";
import {ThemeService} from "../../../services/ThemeService";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-user-parameters',
  templateUrl: './user-parameters.component.html',
  styleUrls: ['./user-parameters.component.css']
})
export class UserParametersComponent implements OnInit {
  parameters: Parameters;
  user: User;
  darkMode: boolean = false;

  constructor(
    private translate: TranslateService,
    private themeService: ThemeService,
    private parametersService: ParametersService,
    private userAccessService: UserAccessService,
    private loggerService: LoggerService
  ) {
  }

  ngOnInit() {
   this.userAccessService.user$.subscribe((user: User) => {
  this.user = user;
});

    this.parametersService.getByUserId(this.user!.id).subscribe(
      (data: any) => {
        if (data) {
          this.parameters = data;
          this.darkMode = this.parameters.theme === "dark"
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
        this.themeService.toggleTheme(this.parameters.theme);
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

  changeTheme() {
    this.darkMode = !this.darkMode;
    this.parameters.theme = !this.darkMode ? 'dark' : 'light';
    this.save();
  }
}
