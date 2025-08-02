import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-content-code-title',
  templateUrl: './content-code-title.component.html',
  styleUrl: './content-code-title.component.css'
})
export class ContentCodeTitleComponent {
  @Input()
  title: string;

  constructor() {
  }

}
