import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentDatasComponent } from './content-datas.component';

describe('ContentDatasComponent', () => {
  let component: ContentDatasComponent;
  let fixture: ComponentFixture<ContentDatasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContentDatasComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ContentDatasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
