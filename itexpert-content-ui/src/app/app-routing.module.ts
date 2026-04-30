import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { NodesComponent } from "./components/node/nodes/nodes.component";
import { LanguagesComponent } from "./components/language/languages/languages.component";
import { UsersComponent } from "./components/user/users/users.component";
import { LoginComponent } from "./components/user/login/login.component";
import { UserInfosComponent } from "./components/user/user-infos/user-infos.component";
import { HelpsComponent } from "./components/ui/helps/helps.component";
import { UserParametersComponent } from "./components/user/user-parameters/user-parameters.component";
import { NotificationsComponent } from "./components/notification/notifications/notifications.component";
import { ContentChartsComponent } from "./components/analytics/content-charts/content-charts.component";
import { AuthGuard } from "./services/AuthGuard";
import { PluginComponent } from "./components/plugins/plugin/plugin.component";
import { ReleaseLocksComponent } from './components/admin/release-locks/release-locks.component';
import { SubscribeComponent } from './components/user/subscribe/subscribe.component';
import { OAuthCallbackComponent } from './components/oauth2/oauth-callback/oauth-callback.component';
import { UnauthorizedComponent } from './components/ui/unauthorized/unauthorized.component';

const routes: Routes =
  [
    {
      path: '',
      component: NodesComponent,
      canActivate: [AuthGuard],
      data: { roles: ['EDITOR', 'READER', 'ADMIN'] }
    },
    {
      path: 'nodes',
      component: NodesComponent,
      canActivate: [AuthGuard],
      data: { roles: ['EDITOR', 'READER', 'ADMIN'] }
    },
    {
      path: 'login',
      component: LoginComponent
    },
    {
      path: 'unauthorized',
      component: UnauthorizedComponent
    },
    {
      path: 'oauth2/callback',
      component: OAuthCallbackComponent
    },
    {
      path: 'openid/callback',
      component: OAuthCallbackComponent
    },
    {
      path: 'subscribe',
      component: SubscribeComponent
    },
    {
      path: 'userInfos',
      component: UserInfosComponent,
      canActivate: [AuthGuard],
      data: { roles: ['EDITOR', 'ADMIN'] }
    },
    {
      path: 'helps',
      component: HelpsComponent
    },
    {
      path: 'parameters',
      component: UserParametersComponent
    },
    {
      path: 'notifications',
      component: NotificationsComponent
    },
    {
      path: 'charts',
      component: ContentChartsComponent,
    },
    {
      path: 'plugins',
      component: PluginComponent
    },
    {
      path: 'admin',
      canActivate: [AuthGuard],
      data: { roles: ['ADMIN'] },
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
export class AppRoutingModule { }