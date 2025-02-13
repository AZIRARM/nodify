import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Node} from "../../../modeles/Node";
import {ContentNode} from "../../../modeles/ContentNode";
import {User} from "../../../modeles/User";

@Component({
  selector: 'app-code-dialog',
  templateUrl: './content-code.component.html',
  styleUrls: ['./content-code.component.css']
})
export class ContentCodeComponent {

  @Input()
  user: User;

  @Input()
  node: Node;

  @Input()
  type: string;


  @Input()
  @Output()
  currentContent: ContentNode;


  @Output() close = new EventEmitter<void>();
  @Output() validate = new EventEmitter<void>();
  @Output() onFileChange = new EventEmitter<void>();


  closeFactory(): void {
    this.close.next();
  }

  validateFactory(): void {
    this.validate.next();
  }

  onFileChangeFactory($event:any): void {
    this.onFileChange.next($event);
  }
}
