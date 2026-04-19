import { Component, Injectable, Input, Output, inject, signal, WritableSignal } from '@angular/core';
import { ContentNode } from "../../../../modeles/ContentNode";
import { SlugService } from 'src/app/services/SlugService';
import { ContentNodeService } from 'src/app/services/ContentNodeService';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-content-code-infos',
  templateUrl: './content-code-infos.component.html',
  styleUrl: './content-code-infos.component.css',
  standalone: false
})
export class ContentCodeInfosComponent {
  @Input()
  contentNode!: ContentNode;

  slugAvailable: WritableSignal<boolean | null> = signal<boolean | null>(null);

  private contentNodeService = inject(ContentNodeService);
  private slugService = inject(SlugService);

  onSlugChange(slug: string) {
    if (!slug) {
      this.slugAvailable.set(null);
      return;
    }

    (this.slugService.exists(slug) as Observable<string[]>)
      .pipe(
        map((codes: (string | null)[]) => {
          const filtered = codes.filter(c => c != null);
          return (
            filtered.length === 0 ||
            (filtered.length === 1 && filtered[0] === this.contentNode.code)
          );
        })
      )
      .subscribe((available: boolean) => {
        this.slugAvailable.set(available);
        if (available) {
          this.contentNode.slug = slug;
        }
      });
  }
}