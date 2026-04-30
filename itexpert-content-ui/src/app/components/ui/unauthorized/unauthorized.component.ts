import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AuthenticationService } from 'src/app/services/AuthenticationService';

@Component({
  selector: 'app-unauthorized',
  templateUrl: './unauthorized.component.html',
  styleUrls: ['./unauthorized.component.css'],
  standalone: false
})
export class UnauthorizedComponent {

  constructor(
    private router: Router,
    private authService: AuthenticationService
  ) { }

  goHome(): void {
    this.router.navigate(['/nodes']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}