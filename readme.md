# Research Project Management System

## Table of Contents
- [System Objectives](#system-objectives)  
- [Authentication](#authentication)  
- [Project Management](#project-management)  
- [Work Package Addition](#work-package-addition)  
- [Task Addition](#task-addition)  
- [Hour Monitoring](#hour-monitoring)  
- [Non-Functional Requirements](#non-functional-requirements)  
- [Business Rules](#business-rules)  
- [Grants](#grants)
- [Authors](#authors)  

---

## System Objectives
The research project management system is designed to automate and simplify the research project lifecycle, from project creation to report generation. The system must ensure compliance with privacy regulations, providing an intuitive and easy-to-use interface.

The system must support the following main features:
- Creation and management of projects, work packages and tasks
- Reporting of working hours by researchers
- Management of user roles and authorizations and credentials

---

## Authentication
If the user is not authenticated the system must redirect to the login page. 
In case of lost credentials, user can request password reset by the administrative staff.

---

## Project Management
Only Administrative staff can add new projects, but the supervisor is the one who manages the project.
Each project must have only one supervisor assigned.
There are no restrictions in the viewing of projects, all users can view all projects.
The supervisor can archive the project.

---

## Work Package Addition
Only the supervisor can add new work packages. No researcher presence required to create a work package.  

---

## Task Addition
Only the supervisor can add new tasks, the tasks may be assigned to one or more researchers in a dynamic way.
The task has a mandatory deadline, also each researcher has a specific time estimate for the task.
The task deadline is valid only if it falls within the work package time span.  
Each task has a specific priority and status, which can be updated by the supervisor or the researcher.

---

## Hour Monitoring
Each researcher can enter only their specific working hours and every entered hour must be linked to a specific task.
Administrators and supervisors do not use this method to enter hours.
The researcher has no time limits in the insertion, because for project needs it may be necessary to consider doing overtime.
Each time entry will be displayed in the details of the specific task.
Each user should be able to extract the information about their own working hours, but not the hours of other users.

---

## Non-Functional Requirements
1. **Performance:** Support at least 50 simultaneous users per instance without performance degradation.  
2. **Scalability:** Able to scale for more users and projects without major architectural changes.  
3. **Lightness:** The system must promote cost containment considering the dependencies and technologies used.
4. **Security:** The system is supposed to run in a private LAN environment.

---

## Business Rules
1. **Project Creation:** Only administrative staff can create new projects.  
2. **Project Management:** A project must have an assigned supervisor.
3. **Project Management:** A project must be archived only by the assigned supervisor.
4. **User Management:** The administrator can add new users and assign specific roles (researcher or scientific manager).

---

## Grants
- Project Creation: Administrative
- Project Reading: Administrative, Supervisor, Researcher
- Work Package Creation: Supervisor
- Work Package Reading: Administrative, Supervisor, Researcher
- Task Creation: Supervisor
- Task Reading: Administrative, Supervisor, Researcher
- Task Editing: Supervisor, Researcher
- Hours Editing: Administrative, Supervisor, Researcher
- Report Creation: Administrative, Supervisor, Researcher

---

## Authors

- Riccardo Peruffo - (https://github.com/RiccardoPeruffo96)