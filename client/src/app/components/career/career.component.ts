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
        'Engage with prospects to understand their business challenges and how Smart Stocks can help.',
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
