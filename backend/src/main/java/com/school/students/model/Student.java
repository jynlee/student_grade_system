package com.school.students.model;

/**
 * 학생 정보 모델.
 * students 테이블의 한 행에 대응한다.
 *
 * - id: DB의 자동 증가 PK
 * - studentNumber: 학번 (UNIQUE 제약)
 * - name: 이름
 * - department: 학과
 * - gradeYear: 학년 (1~4, DB CHECK 제약)
 *
 * Jackson이 getter를 통해 JSON 직렬화하므로 필드명이 JSON 키가 된다.
 */
public class Student {
    private Long id;
    private String studentNumber;
    private String name;
    private String department;
    private Integer gradeYear;

    public Student() {}

    public Student(Long id, String studentNumber, String name, String department, Integer gradeYear) {
        this.id = id;
        this.studentNumber = studentNumber;
        this.name = name;
        this.department = department;
        this.gradeYear = gradeYear;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Integer getGradeYear() { return gradeYear; }
    public void setGradeYear(Integer gradeYear) { this.gradeYear = gradeYear; }
}
