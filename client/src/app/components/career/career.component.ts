import { Component, OnInit } from '@angular/core';
import { EventTrackingService } from '../../services/event-tracking.service';

@Component({
  selector: 'app-career',
  templateUrl: './career.component.html',
  styleUrls: ['./career.component.css']
})
export class CareerComponent implements OnInit {

  jobs = [
    {
      title: 'Sales Development Representative (SDR)',
      department: 'Sales',
      location: 'Remote',
      type: 'Full-time',
      description: 'We are looking for a highly motivated Sales Development Representative to join our fast-growing team. You will be the first point of contact for our potential customers and play a crucial role in our sales cycle.',
      responsibilities: [
        'Identify and qualify new sales opportunities through outbound calling, email, and social media.',
        'Engage with prospects to understand their business challenges and how Herculean can help.',
        'Schedule discovery calls and meetings for Account Executives.',
        'Maintain a well-organized pipeline and update our CRM system regularly.',
        'Collaborate with marketing on campaigns and messaging strategies.'
      ],
      requirements: [
        'Proven experience as an SDR, BDR, or in a similar sales role.',
        'Strong communication and interpersonal skills.',
        'Familiarity with CRM tools (e.g., Salesforce, HubSpot).',
        'Ability to understand customer needs and handle objections effectively.',
        'Self-motivated, goal-oriented, and comfortable working in a fast-paced environment.'
      ]
    },
    {
      title: 'Angular Frontend Intern',
      department: 'Engineering',
      location: 'Remote',
      type: 'Internship',
      description: 'We are looking for a passionate Angular Frontend Intern to help build next-generation user interfaces for Herculean. You will work closely with our senior engineers and designers to deliver stunning, performant web applications.',
      responsibilities: [
        'Develop and maintain user-facing features using Angular and TypeScript.',
        'Implement responsive, pixel-perfect UI components based on Figma designs.',
        'Collaborate with backend engineers to integrate RESTful APIs.',
        'Write clean, maintainable code and participate in code reviews.',
        'Optimize application performance and ensure a seamless user experience.'
      ],
      requirements: [
        'Basic understanding of Angular, TypeScript, HTML, and modern CSS/SCSS.',
        'Familiarity with RxJS and state management concepts.',
        'Strong problem-solving skills and eagerness to learn new technologies.',
        'Good communication skills and ability to work in a remote team environment.',
        'Currently pursuing or recently completed a degree in Computer Science or a related field.'
      ]
    },
    {
      title: 'Java Backend Intern',
      department: 'Engineering',
      location: 'Remote',
      type: 'Internship',
      description: 'Join our backend team as a Java Backend Intern and help power the core engine of the Herculean platform. You will gain hands-on experience building scalable microservices and working with massive financial datasets.',
      responsibilities: [
        'Assist in designing and developing robust RESTful APIs using Java and Spring Boot.',
        'Write efficient database queries and optimize data retrieval processes.',
        'Participate in the architecture and design of scalable backend systems.',
        'Write unit and integration tests to ensure code quality and reliability.',
        'Troubleshoot and resolve backend bugs and performance issues.'
      ],
      requirements: [
        'Solid foundation in Java programming and Object-Oriented Design principles.',
        'Familiarity with Spring Boot and REST API concepts.',
        'Basic understanding of relational databases (e.g., MySQL, PostgreSQL) and SQL.',
        'Understanding of version control systems like Git.',
        'Strong analytical skills and a passion for backend engineering.'
      ]
    }
  ];

  constructor(private eventTracker: EventTrackingService) { }

  ngOnInit(): void {
    window.scrollTo(0, 0);
  }

  apply(jobTitle: string) {
    const name = window.prompt("Please enter your name:");
    if (!name) return;
    
    const email = window.prompt("Please enter your email:");
    if (!email) return;

    const phone = window.prompt("Please enter your phone number:");
    if (!phone) return;

    this.eventTracker.trackEvent('JOB_APPLICATION', {
      jobTitle: jobTitle,
      applicantName: name,
      applicantEmail: email,
      applicantPhone: phone
    });

    alert(`Thank you, ${name}! Your interest in the ${jobTitle} position has been recorded.`);
  }
}
