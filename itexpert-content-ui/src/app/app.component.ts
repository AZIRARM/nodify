import {Component, HostListener} from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  headerHidden = false;
  footerHidden = false;
  private hideTimer: any;

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(e: MouseEvent) {
    if (e.clientY < 20) {
      this.showHeader();
    }
    if (e.clientY > window.innerHeight - 20) {
      this.showFooter();
    }
  }

  showHeader() {
    this.headerHidden = false;
    this.resetTimer();
  }

  showFooter() {
    this.footerHidden = false;
    this.resetTimer();
  }

  resetTimer() {
    clearTimeout(this.hideTimer);
    this.hideTimer = setTimeout(() => {
      this.headerHidden = true;
      this.footerHidden = true;
    }, 2000);
  }
}
