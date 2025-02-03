import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HelpsComponent } from './helps.component';

describe('HelpsComponent', () => {
  let component: HelpsComponent;
  let fixture: ComponentFixture<HelpsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [HelpsComponent]
    });
    fixture = TestBed.createComponent(HelpsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
