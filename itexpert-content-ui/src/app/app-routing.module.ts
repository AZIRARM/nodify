import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {AppComponent} from "./app.component";
import {NodesComponent} from "./components/node/nodes/nodes.component";
import {LanguagesComponent} from "./components/language/languages/languages.component";
import {UsersComponent} from "./components/user/users/users.component";
import {LoginComponent} from "./components/ui/login/login.component";
import {UserInfosComponent} from "./components/user/user-infos/user-infos.component";
import {HelpsComponent} from "./components/ui/helps/helps.component";
import {UserParametersComponent} from "./components/user/user-parameters/user-parameters.component";
import {DeletedNodesDialogComponent} from "./components/node/deleted-nodes-dialog/deleted-nodes-dialog.component";
import {
  DeletedContentsNodesDialogComponent
} from "./components/content-node/deleted-contents-nodes-dialog/deleted-contents-nodes-dialog.component";
import {NotificationsComponent} from "./components/notification/notifications/notifications.component";
import {ContentChartsComponent} from "./components/analytics/content-charts/content-charts.component";
import {AuthGuard} from "./services/AuthGuard";
import {PluginComponent} from "./components/plugins/plugin/plugin.component";

const routes: Routes =
  [
    {
      path: '',
      component: AppComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'nodes',
      component: NodesComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'languages',
      component: LanguagesComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'users',
      component: UsersComponent,
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
      path: 'deleteNodes',
      component: DeletedNodesDialogComponent,
      canActivate: [AuthGuard]
    },
    {
      path: 'deleteContents',
      component: DeletedContentsNodesDialogComponent,
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

  ];

@NgModule({
  imports: [RouterModule.forRoot(routes, {
    onSameUrlNavigation: 'reload',
  })],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
