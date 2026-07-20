import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-buffett-lessons',
  templateUrl: './buffett-lessons.component.html',
  styleUrls: ['./buffett-lessons.component.css']
})
export class BuffettLessonsComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
    window.scrollTo(0, 0);
  }

}
