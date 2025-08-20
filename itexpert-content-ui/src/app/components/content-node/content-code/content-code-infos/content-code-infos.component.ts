import { Component, Injectable, Input, Output } from '@angular/core';
import { ContentNode } from "../../../../modeles/ContentNode";
import { SlugService } from 'src/app/services/SlugService';
import { ContentNodeService } from 'src/app/services/ContentNodeService';
import { toArray, map } from 'rxjs/operators';
import { Observable } from 'rxjs';

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

  (this.slugService.exists(slug) as Observable<string[]>)
    .pipe(
      map((codes: (string | null)[]) => {
        const filtered = codes.filter(c => c != null); // supprime null / undefined
        return (
          filtered.length === 0 ||
          (filtered.length === 1 && filtered[0] === this.contentNode.code)
        );
      })
    )
    .subscribe((available: boolean) => {
      this.slugAvailable = available;
      if (available) {
        this.contentNode.slug = slug;
      }
    });
  }
}
