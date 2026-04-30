import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { Parameters } from "../../../modeles/Parameters";
import { TranslateService } from "@ngx-translate/core";
import { LoggerService } from "../../../services/LoggerService";
import { ParametersService } from "../../../services/ParametersService";
import { User } from "../../../modeles/User";
import { ThemeService } from "../../../services/ThemeService";
import { UserAccessService } from "../../../services/UserAccessService";
import { catchError, finalize, switchMap } from "rxjs/operators";
import { of } from "rxjs";

@Component({
  selector: 'app-user-parameters',
  templateUrl: './user-parameters.component.html',
  styleUrls: ['./user-parameters.component.css'],
  standalone: false
})
export class UserParametersComponent implements OnInit {
  parameters: WritableSignal<Parameters> = signal<Parameters>(new Parameters());
  user: WritableSignal<User> = signal<User>({} as User);
  darkMode: WritableSignal<boolean> = signal(false);


  private translate = inject(TranslateService);
  private themeService = inject(ThemeService);
  private parametersService = inject(ParametersService);
  private userAccessService = inject(UserAccessService);
  private loggerService = inject(LoggerService);

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.loadParameters();
  }

  loadParameters() {

    this.parametersService.getByUserId(this.user().id).pipe(
      catchError((error) => {
        console.error('Erreur chargement paramètres', error);
        return of(null);
      }),

    ).subscribe((data: any) => {
      if (data) {
        this.parameters.set(data);
        this.darkMode.set(this.parameters().theme === "dark");
      }
    });
  }

  save() {

    this.parametersService.save(this.parameters()).pipe(
      switchMap((data: any) => {
        this.themeService.toggleTheme(this.parameters().theme);
        if (data) {
          return this.translate.get("SAVE_SUCCESS");
        }
        return of(null);
      }),
      catchError((error) => {
        return this.translate.get("SAVE_ERROR").pipe(
          switchMap((trad: string) => {
            this.loggerService.error(trad);
            throw error;
          })
        );
      }),

    ).subscribe((trad: string | null) => {
      if (trad) {
        this.loggerService.success(trad);
      }
    });
  }

  changeTheme() {
    this.darkMode.set(!this.darkMode());
    const currentParams = this.parameters();
    currentParams.theme = this.darkMode() ? 'dark' : 'light';
    this.parameters.set(currentParams);
    this.save();
  }
}