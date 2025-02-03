import {Injectable} from "@angular/core";
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from "@angular/common/http";
import {Observable} from "rxjs";
import {Router} from "@angular/router";
import {tap} from 'rxjs/operators';

@Injectable()
export class TokenInterceptor implements HttpInterceptor {

  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const userToken = 'secure-user-token';
    const modifiedReq = req.clone({
      headers: req.headers
        .set('Authorization', 'Bearer ' + this.getAuthorization())
        .set('Content-Type', 'application/json')
        .set('Accept', 'application/json')
    });
    return next.handle(modifiedReq).pipe( tap(() => {},
      (err: any) => {
        if (err instanceof HttpErrorResponse) {
         // if (err.status !== 401) {
         //   return;
         // }
         // this.router.navigate(['login']);
        }
      }));
  }



  getAuthorization() {
    return (window.localStorage.getItem("userToken")
      ?
      JSON.parse((JSON.parse(JSON.stringify(window.localStorage.getItem("userToken"))))).token
      :
      '');
  }


}
