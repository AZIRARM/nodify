// footer.component.ts
import {Component, OnInit} from '@angular/core';

@Component({
    selector: 'app-footer',
    templateUrl: './footer.component.html',
    styleUrls: ['./footer.component.css'],
    standalone: false
})
export class FooterComponent implements OnInit {
  date: string;
  currentYear = new Date().getFullYear();
  isFooterVisible = false;
  private hideTimer: any;
  private isMouseOverFooter = false;

  ngOnInit(): void {
    this.date = this.getCurrentDate();

    document.addEventListener('mousemove', (e) => {
      if (e.clientY > window.innerHeight - 20) {
        this.showFooter();
      }
    });
  }

  showFooter() {
    this.cancelHideTimer();
    this.isFooterVisible = true;
  }

  onMouseEnter() {
    this.isMouseOverFooter = true;
    this.cancelHideTimer();
  }

  onMouseLeave() {
    this.isMouseOverFooter = false;
    this.startHideTimer();
  }

  startHideTimer() {
    this.hideTimer = setTimeout(() => {
      if (!this.isMouseOverFooter) {
        this.isFooterVisible = false;
      }
    }, 2000);
  }

  cancelHideTimer() {
    if (this.hideTimer) {
      clearTimeout(this.hideTimer);
      this.hideTimer = null;
    }
  }

  getCurrentDate() {
    const t = new Date();
    const date = ('0' + t.getDate()).slice(-2);
    const month = ('0' + (t.getMonth() + 1)).slice(-2);
    const year = t.getFullYear();
    return `${date}/${month}/${year}`;
  }
}
