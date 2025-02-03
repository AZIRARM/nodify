import {Component, OnInit, ViewChild} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {ToastrService} from "ngx-toastr";
import {MatSidenav} from "@angular/material/sidenav";
import {SidenavService} from "../../../services/SidenavService";
import {UserAccessService} from "../../../services/UserAccessService";

@Component({
  selector: 'app-sidenav',
  templateUrl: './sidenav.component.html',
  styleUrls: ['./sidenav.component.css']
})
export class SidenavComponent implements OnInit{

  user:any;

  @ViewChild('sideNav',{static:true}) sideNav: any;
  constructor(translate: TranslateService,
              toast: ToastrService,
              public userAccessService: UserAccessService,
              private sidenavService: SidenavService) {
  }

  ngOnInit(): void {
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify( ( window.localStorage.getItem('userInfo') ) )
      )
    );
    this.sidenavService.setSidenav(this.sideNav);
  }

}
