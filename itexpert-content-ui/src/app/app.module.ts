import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

import {HTTP_INTERCEPTORS, HttpClient, HttpClientModule} from '@angular/common/http';
import {TranslateLoader, TranslateModule} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';


import {MatButtonModule} from '@angular/material/button';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatMenuModule} from '@angular/material/menu';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatIconModule} from '@angular/material/icon';
import {MatListModule} from '@angular/material/list';
import {RouterModule} from '@angular/router';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatTableModule} from '@angular/material/table'
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatSelectModule} from '@angular/material/select';
import {MatInputModule} from "@angular/material/input";
import {MatPaginatorModule} from "@angular/material/paginator";
import {MatSortModule} from '@angular/material/sort';
import {MatDialogModule} from '@angular/material/dialog';

import {ToastrModule} from 'ngx-toastr';
import {NodesComponent} from './components/node/nodes/nodes.component';
import {UsersComponent} from './components/user/users/users.component';
import {NotificationsComponent} from './components/notification/notifications/notifications.component';
import {HeaderComponent} from './components/ui/header/header.component';
import {FooterComponent} from './components/ui/footer/footer.component';
import {SidenavComponent} from './components/ui/sidenav/sidenav.component';

import {SidenavService} from "./services/SidenavService";
import {NodeService} from "./services/NodeService";
import {ContentNodeService} from "./services/ContentNodeService";
import {LanguageService} from "./services/LanguageService";
import {LoggerService} from './services/LoggerService';
import {RoleService} from './services/RoleService';
import {AuthenticationService} from './services/AuthenticationService';


import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ValidationDialogComponent} from './components/commons/validation-dialog/validation-dialog.component';
import {LanguagesComponent} from './components/language/languages/languages.component';
import {LanguageDialogComponent} from './components/language/language-dialog/language-dialog.component';
import {UserDialogComponent} from './components/user/user-dialog/user-dialog.component';
import {NodeDialogComponent} from "./components/node/node-dialog/node-dialog.component";
import {UserService} from "./services/UserService";
import {LoginComponent} from './components/ui/login/login.component';
import {UserInfosComponent} from './components/user/user-infos/user-infos.component';
import {PasswordDialogComponent} from "./components/user/password-dialog/password-dialog.component";
import {HelpsComponent} from './components/ui/helps/helps.component';
import {UserParametersComponent} from './components/user/user-parameters/user-parameters.component';
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {ParametersService} from "./services/ParametersService";
import {NotificationService} from "./services/NotificationService";
import {TokenInterceptor} from "./interceptors/TokenInterceptor";
import {ContentNodeDialogComponent} from './components/content-node/content-node-dialog/content-node-dialog.component';
import {AccessRoleService} from "./services/AccessRoleService";
import {ValuesDialogComponent} from './components/commons/values-dialog/values-dialog.component';
import {NodeAccessRolesDialogComponent} from './components/node/node-access-roles-dialg/node-access-roles-dialog.component';
import {
  NodeRulesConditionsDialogComponent
} from './components/commons/node-rules-conditions-dialog/node-rules-conditions-dialog.component';
import {MatDatepickerModule} from "@angular/material/datepicker";
import {PublishedNodesDialogComponent} from "./components/node/published-nodes-dialog/published-nodes-dialog.component";
import {
  PublishedContentsNodesDialogComponent
} from './components/content-node/published-contents-nodes-dialog/published-contents-nodes-dialog.component';
import {TranslationsDialogComponent} from './components/commons/translations-dialog/translations-dialog.component';
import {DeletedNodesDialogComponent} from './components/node/deleted-nodes-dialog/deleted-nodes-dialog.component';
import {
  DeletedContentsNodesDialogComponent
} from './components/content-node/deleted-contents-nodes-dialog/deleted-contents-nodes-dialog.component';
import {UserNamePipe} from './pipes/user-name.pipe';
import {MatBadgeModule} from "@angular/material/badge";
import {FeedbackService} from "./services/FeedbackService";
import {NgxChartsModule} from "@swimlane/ngx-charts";
import {ContentClickService} from "./services/ContentClickService";
import {ContentChartsComponent} from './components/analytics/content-charts/content-charts.component';
import {ContentDisplayService} from "./services/ContentDisplayService";
import {UserAccessService} from "./services/UserAccessService";
import {AuthGuard} from "./services/AuthGuard";
import {TypeofPipe} from "./pipes/TypeofPipe";
import {MatCardModule} from "@angular/material/card";
import {MatChipsModule} from "@angular/material/chips";
import {CodemirrorModule} from "@ctrl/ngx-codemirror";
import {ContentDatasComponent} from "./components/content-node/content-datas/content-datas.component";
import {ThemeService} from "./services/ThemeService";

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http);
}

export function defaultLanguage (){
  let lang:string =  window.localStorage.getItem("defaultLanguage")!;
  if(!lang) {
    lang = "en";
  }
  return lang;
}

@NgModule({
  declarations: [
    AppComponent,
    NodesComponent,
    NodeDialogComponent,
    UsersComponent,
    NotificationsComponent,
    HeaderComponent,
    FooterComponent,
    SidenavComponent,
    ValidationDialogComponent,
    LanguagesComponent,
    LanguageDialogComponent,
    PasswordDialogComponent,
    UserDialogComponent,
    LoginComponent,
    UserInfosComponent,
    HelpsComponent,
    UserParametersComponent,
    ContentNodeDialogComponent,
    ValuesDialogComponent,
    NodeAccessRolesDialogComponent,
    NodeRulesConditionsDialogComponent,
    PublishedNodesDialogComponent,
    PublishedContentsNodesDialogComponent,
    TranslationsDialogComponent,
    DeletedNodesDialogComponent,
    DeletedContentsNodesDialogComponent,
    UserNamePipe,
    ContentChartsComponent,
    TypeofPipe,
    ContentDatasComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,

    HttpClientModule,
    ToastrModule.forRoot({
      timeOut: 3000,
      positionClass: 'toast-top-full-width',
      preventDuplicates: true,
    }),

    TranslateModule.forRoot({
      defaultLanguage: defaultLanguage(),
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [HttpClient],
      },
    }),

    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatSidenavModule,
    MatMenuModule,
    MatToolbarModule,
    MatIconModule,
    MatListModule,
    RouterModule,
    MatExpansionModule,
    MatTooltipModule,
    MatTableModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    MatSortModule,
    MatDialogModule,
    MatSlideToggleModule,
    MatDatepickerModule,
    MatBadgeModule,
    MatCardModule,
    MatChipsModule,
    NgxChartsModule,
    CodemirrorModule,
    MatIconModule,
    MatTooltipModule
  ],

  providers: [
    {provide: HTTP_INTERCEPTORS, useClass: TokenInterceptor, multi: true},
    LoggerService,
    SidenavService,
    AuthenticationService,
    NodeService,
    ContentNodeService,
    LanguageService,
    RoleService,
    UserService,
    ParametersService,
    NotificationService,
    AccessRoleService,
    FeedbackService,
    ContentClickService,
    ContentDisplayService,
    UserAccessService,
    ThemeService,
    AuthGuard
  ],

  bootstrap: [AppComponent]


})
export class AppModule {
}
