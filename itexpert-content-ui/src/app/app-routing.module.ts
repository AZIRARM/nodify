import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {AppComponent} from "./app.component";
import {NodesComponent} from "./components/node/nodes/nodes.component";
import {LanguagesComponent} from "./components/language/languages/languages.component";
import {UsersComponent} from "./components/user/users/users.component";
import {LoginComponent} from "./components/user/login/login.component";
import {UserInfosComponent} from "./components/user/user-infos/user-infos.component";
import {HelpsComponent} from "./components/ui/helps/helps.component";
import {UserParametersComponent} from "./components/user/user-parameters/user-parameters.component";
import {NotificationsComponent} from "./components/notification/notifications/notifications.component";
import {ContentChartsComponent} from "./components/analytics/content-charts/content-charts.component";
import {AuthGuard} from "./services/AuthGuard";
import {PluginComponent} from "./components/plugins/plugin/plugin.component";
import { ReleaseLocksComponent } from './components/admin/release-locks/release-locks.component';

const routes: Routes =
  [
    {
      path: '',
      component: NodesComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'nodes',
      component: NodesComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'login',
      component: LoginComponent
    },
    {
      path: 'userInfos',
      component: UserInfosComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'helps',
      component: HelpsComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'parameters',
      component: UserParametersComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'notifications',
      component: NotificationsComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'charts',
      component: ContentChartsComponent,
      canActivate: [AuthGuard]
    },

    {
      path: 'plugins',
      component: PluginComponent,
      canActivate: [AuthGuard]
    },
    {
        path: 'admin',
        canActivate: [AuthGuard],
        children: [
          { path: 'users', component: UsersComponent },
          { path: 'languages', component: LanguagesComponent },
          { path: 'unlock-resources', component: ReleaseLocksComponent }
        ]
      }

  ];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    onSameUrlNavigation: 'reload',
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
