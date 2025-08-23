import { Injectable } from "@angular/core";
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { Observable } from "rxjs";
import { Router } from "@angular/router";
import { tap, finalize } from 'rxjs/operators';
import { AuthenticationService } from "../services/AuthenticationService";
import {LoaderService} from "../services/Loader.service";

@Injectable()
export class TokenInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    private authService: AuthenticationService,
    private loaderService: LoaderService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getAccessToken();

    let headers = req.headers
      .set('Content-Type', 'application/json')
      .set('Accept', 'application/json');

    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    const modifiedReq = req.clone({ headers });

    // Montre le loader
    this.loaderService.show();

    return next.handle(modifiedReq).pipe(
      tap({
        error: (err: any) => {
          if (err instanceof HttpErrorResponse && !this.authService.isAuthenticated()) {
            this.router.navigate(['/login']);
          }
        }
      }),
      finalize(() => {
        // Cache le loader après la requête (succès ou erreur)
        this.loaderService.hide();
      })
    );
  }
}
