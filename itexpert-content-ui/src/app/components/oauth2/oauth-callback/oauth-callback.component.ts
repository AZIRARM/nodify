import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ToastrService } from 'ngx-toastr';
import { AuthenticationService } from 'src/app/services/AuthenticationService';
import { CookiesService } from 'src/app/services/CookiesService';

@Component({
  selector: 'app-oauth-callback',
  standalone: false,
  templateUrl: './oauth-callback.component.html',
  styleUrl: './oauth-callback.component.css'
})
export class OAuthCallbackComponent implements OnInit {
  errorMessage = signal<string | null>(null);

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthenticationService);
  private cookiesService = inject(CookiesService);

  private translate = inject(TranslateService);
  private toast = inject(ToastrService);

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const error = params['error'];

      if (error) {
        console.error('OAuth error:', error);
        this.errorMessage.set(error);
        this.router.navigateByUrl('/login?error=' + error);
        return;
      }

      if (token) {
        console.log('Token received, storing...');
        this.authService.setTokens(token);
        this.cookiesService.setCookie("userInfos", "", -1);

        this.authService.loadUser().subscribe({
          next: () => {
            console.log('User loaded, redirecting to nodes');
            this.router.navigateByUrl('/nodes');
          },
          error: (err) => {
            console.error('Failed to load user:', err);
            this.errorMessage.set('load_user_failed');
            this.translate.get("LOAD_USER_FAILED").subscribe((translation: string) => {
              this.toast.error(translation);
            });

            this.router.navigateByUrl('/login?error=load_user_failed');
          }
        });
      } else {
        console.error('No token in callback');
        this.router.navigateByUrl('/login?error=no_token');
      }
    });
  }
}