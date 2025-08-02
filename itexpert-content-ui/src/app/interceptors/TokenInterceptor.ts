import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import {BehaviorSubject, Observable, tap, throwError} from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthenticationService } from "../services/AuthenticationService";

@Injectable()
export class TokenInterceptor implements HttpInterceptor {

  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

  constructor(
    private authService: AuthenticationService,
    private router: Router
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.authService.getAccessToken();

    // Ajoute seulement Authorization, sans toucher aux autres headers
    const authReq = req.clone({
        headers: req.headers
          .set('Authorization', 'Bearer ' + token)
          .set('Content-Type', 'application/json')
          .set('Accept', 'application/json')

    });

    return next.handle(authReq).pipe(
      tap(() => {}, (err: any) => {
        if (err instanceof HttpErrorResponse) {
          // gestion erreur éventuelle
        }
      })
    );
  }


  private addToken(req: HttpRequest<any>, token: string | null): HttpRequest<any> {
    if (!token) return req;

    // Ajoute ou remplace uniquement l’en-tête Authorization, sans toucher aux autres headers
    return req.clone({
      headers: req.headers
        .set('Authorization', 'Bearer ' + token)
        .set('Content-Type', 'application/json')
        .set('Accept', 'application/json')
    });
  }

  private handle401Error(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken(this.authService.getRefreshToken()).pipe(
        switchMap((res: any) => {
          this.isRefreshing = false;

          const newAccessToken = res.accessToken;
          const newRefreshToken = res.refreshToken;

          this.authService.setTokens(newAccessToken, newRefreshToken);
          this.refreshTokenSubject.next(newAccessToken);

          return next.handle(this.addToken(req, newAccessToken));
        }),
        catchError(err => {
          this.isRefreshing = false;
          this.authService.logout();
          this.router.navigate(['/login']);
          return throwError(() => err);
        })
      );
    } else {
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => next.handle(this.addToken(req, token!)))
      );
    }
  }
}
