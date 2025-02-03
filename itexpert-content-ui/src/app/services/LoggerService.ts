import { ToastrService } from 'ngx-toastr';
import {Injectable} from "@angular/core";


@Injectable()
export class LoggerService {
  constructor(private toastr: ToastrService) {}

  success(message:string) {
    this.toastr.success(message, '');
  }
  error(message:string) {
    this.toastr.error(message, '');
  }
  warn(message:string) {
    this.toastr.warning(message, '');
  }
}
