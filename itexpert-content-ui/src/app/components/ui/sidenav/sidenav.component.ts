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
              private userAccessService: UserAccessService) {
  }

  ngOnInit() {
    this.userAccessService.user$.subscribe((user: any) => {
      this.user = user;
    });
    this.sidenavService.setSidenav(this.sideNav);
  }

}
