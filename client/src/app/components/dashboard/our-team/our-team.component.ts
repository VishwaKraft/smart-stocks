import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-our-team',
  templateUrl: './our-team.component.html',
  styleUrls: ['./our-team.component.css'],
})
export class OurTeamComponent implements OnInit {
  team = [
    {
      name: 'Jeevith Ravi Kumar',
      position: 'Frontend Developer',
      image: 'assets/jeevith.jpg',
    },
    {
      name: 'Amol Saini',
      position: 'Full Stack Developer',
      image: 'assets/amol.jpg',
    },
    {
      name: 'Vishwas Atrey',
      position: 'Java Developer',
      image:
        'https://media-exp1.licdn.com/dms/image/C5103AQEMTwOo6e31Bw/profile-displayphoto-shrink_200_200/0/1578424431605?e=1636588800&v=beta&t=L7YdoYBYmZnSh_MW0C17FFOglQDx5zGBHqj5DVl5wD8',
    },
    {
      name: 'Aishwarya',
      position: 'Data Scientist',
      image: 'assets/aish.jpeg',
    },
    {
      name: 'Ripudaman Singh',
      position: 'Full Stack Developer',
      image: 'assets/ripu.jpeg',
    },
    {
      name: 'Gaurav Khurana',
      position: 'Mentor',
      image: 'assets/gaurav.jfif',
    },
  ];

  constructor() {}

  ngOnInit(): void {}
}
