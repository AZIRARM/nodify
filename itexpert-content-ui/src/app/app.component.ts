import {Component, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {LoggerService} from "./services/LoggerService";
import {Parameters} from "./modeles/Parameters";
import {ParametersService} from "./services/ParametersService";
import {AuthenticationService} from "./services/AuthenticationService";
import {User} from "./modeles/User";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {


  parameters: Parameters;
  user: User;

  constructor(
    private translate: TranslateService,
    private loggerService: LoggerService,
    private parametersService: ParametersService,
    private authService: AuthenticationService
  ) {

  }

  ngOnInit() {
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userToken')))
      )
    );

    this.init();
  }

  private init() {
    this.authService.getConnectedUser().subscribe((connectedUser:User)=>{
      this.parametersService.getByUserId(connectedUser.id).subscribe(
        (data: any) => {
          if (data) {
            this.parameters = data;
          }
        },
        error => {
          console.error(error);
        }
      );
    })
  }
}
