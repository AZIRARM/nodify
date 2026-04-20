import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationService } from 'src/app/services/AuthenticationService';

@Component({
  selector: 'app-oauth-callback',
  standalone: false,
  templateUrl: './oauth-callback.component.html',
  styleUrl: './oauth-callback.component.css'
})
export class OAuthCallbackComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthenticationService
  ) { }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const error = params['error'];

      if (error) {
        console.error('OAuth error:', error);
        this.router.navigateByUrl('/login?error=' + error);
        return;
      }

      if (token) {
        console.log('Token received, storing...');
        this.authService.setTokens(token);
        this.authService.loadUser().subscribe({
          next: () => {
            console.log('User loaded, redirecting to nodes');
            this.router.navigateByUrl('/nodes');
          },
          error: (err) => {
            console.error('Failed to load user:', err);
            this.router.navigateByUrl('/login?error=load_user_failed');
          }
        });
      } else {
        console.error('No token in callback');
        this.router.navigateByUrl('/nodes');
      }
    });
  }
}