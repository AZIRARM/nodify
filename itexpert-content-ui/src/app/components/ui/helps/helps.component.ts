import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-helps',
  templateUrl: './helps.component.html',
  styleUrls: ['./helps.component.css']
})
export class HelpsComponent  implements OnInit {
  markdownContent: any = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadMarkdown();
  }

  loadMarkdown() {

  }
}
