import {Component, OnInit, ViewChild} from '@angular/core';
import {SidenavService} from "../../../services/SidenavService";

@Component({
  selector: 'app-sidenav',
  templateUrl: './sidenav.component.html',
  styleUrls: ['./sidenav.component.css']
})
export class SidenavComponent implements OnInit {

  user: any;

  @ViewChild('sideNav', {static: true}) sideNav: any;

  constructor(private sidenavService: SidenavService) {
  }

  ngOnInit(): void {
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userInfo')))
      )
    );
    this.sidenavService.setSidenav(this.sideNav);
  }

}
