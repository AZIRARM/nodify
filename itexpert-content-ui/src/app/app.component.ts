import {Component, OnInit} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {LoggerService} from "./services/LoggerService";
import {Parameters} from "./modeles/Parameters";
import {ParametersService} from "./services/ParametersService";
import {AuthenticationService} from "./services/AuthenticationService";
import {User} from "./modeles/User";
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, switchMap, of} from 'rxjs';

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
    private authService: AuthenticationService,
    private router: Router
  ) { }

  ngOnInit() {
    this.init();
  }

  private init() {
      this.authService.getConnectedUser().pipe(
        switchMap((connectedUser: User) => {
          if (!connectedUser) {
            this.router.navigate(['/login']);
            return of(null);
          }
          this.user = connectedUser;
          return this.parametersService.getByUserId(connectedUser.id) as Observable<Parameters | null>;
        })
      ).subscribe(
        (data: Parameters | null) => {
          if (data) {
            this.parameters = data;
          }
          this.router.navigate(['/nodes']);
        },
        (error) => {
          console.error(error);
          this.router.navigate(['/login']);
        }
      );
  }
}

