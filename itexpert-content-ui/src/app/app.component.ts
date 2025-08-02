import {Component} from '@angular/core';
import {UserAccessService} from "./services/UserAccessService";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {


  constructor(private userAccessService:UserAccessService) {
    this.userAccessService.init();
  }

}
