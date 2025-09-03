import {Component, OnInit, ViewChild} from '@angular/core';
import {SidenavService} from "../../../services/SidenavService";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-sidenav',
  templateUrl: './sidenav.component.html',
  styleUrls: ['./sidenav.component.css']
})
export class SidenavComponent implements OnInit {

  user: any;

  @ViewChild('sideNav', {static: true}) sideNav: any;

  constructor(private sidenavService: SidenavService,
              public userAccessService: UserAccessService) {
  }

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser()
    this.sidenavService.setSidenav(this.sideNav);
  }

  isAdmin(){
    return this.userAccessService.isAdmin();
  }
}
