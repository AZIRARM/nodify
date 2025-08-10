import { Component, Injectable, Input, Output } from '@angular/core';
import { ContentNode } from "../../../../modeles/ContentNode";
import { map, Observable } from 'rxjs';
import { SlugService } from 'src/app/services/SlugService';
import { ContentNodeService } from 'src/app/services/ContentNodeService';

@Component({
  selector: 'app-content-code-infos',
  templateUrl: './content-code-infos.component.html',
  styleUrl: './content-code-infos.component.css'
})
export class ContentCodeInfosComponent {
  @Input()
  contentNode!: ContentNode;

  slugAvailable: boolean | null = null;

  constructor(
    private contentNodeService: ContentNodeService,
    private slugService: SlugService) {}

  onSlugChange(slug: string) {
    if (!slug) {
      this.slugAvailable = null;
      return;
    }

    /*this.slugService.exists(slug)
      .pipe(map((res: any) => !res))
      .subscribe(isAvailable => {
        this.slugAvailable = isAvailable;
      });*/


   this.slugService.exists(slug)
    .subscribe((exists: any)=>{
      if(exists) {
        this.contentNodeService.slugExists(this.contentNode.code, slug)
          .subscribe((existsForContent: any) => {
            if (existsForContent === false) {
              this.slugAvailable = false;
            } else {
              this.slugAvailable = true;
            }
         });
      } else {
              this.slugAvailable = true;
      }

    });
  
  }
}
